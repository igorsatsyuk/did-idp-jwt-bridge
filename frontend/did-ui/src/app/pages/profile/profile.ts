import { JsonPipe, NgIf } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { finalize } from 'rxjs';

import { Api, ProfileResponse } from '../../core/api';
import { readAccessToken } from '../../core/auth-session';
import { Router } from '@angular/router';

@Component({
  selector: 'app-profile',
  imports: [MatCardModule, MatButtonModule, NgIf, JsonPipe],
  templateUrl: './profile.html',
  styleUrl: './profile.scss'
})
export class Profile implements OnInit {
  private readonly api = inject(Api);
  private readonly router = inject(Router);

  profile: ProfileResponse | null = null;
  isLoading = false;
  successMessage: string | null = null;
  errorMessage: string | null = null;

  ngOnInit(): void {
    this.loadProfile();
  }

  loadProfile(): void {
    this.profile = null;
    this.successMessage = null;
    this.errorMessage = null;

    const token = readAccessToken();
    if (token === null) {
      this.errorMessage = 'JWT not found in session storage. Please authenticate first.';
      void this.router.navigate(['/auth']);
      return;
    }

    this.isLoading = true;
    this.api
      .getProfile(token)
      .pipe(finalize(() => (this.isLoading = false)))
      .subscribe({
        next: (profile) => {
          this.profile = profile;
          this.successMessage = 'Protected profile loaded successfully.';
        },
        error: (error: HttpErrorResponse) => {
          if (error.status === 401) {
            this.errorMessage = 'JWT is missing or expired. Redirecting to auth flow.';
            void this.router.navigate(['/auth']);
            return;
          }
          this.errorMessage = this.formatErrorMessage(error, 'Could not load protected profile');
        }
      });
  }

  private formatErrorMessage(error: HttpErrorResponse, fallback: string): string {
    if (typeof error.error === 'string' && error.error.trim().length > 0) {
      const maybeJsonMessage = this.extractMessageFromJson(error.error);
      if (maybeJsonMessage !== null) {
        return maybeJsonMessage;
      }
      return error.error;
    }

    if (
      typeof error.error === 'object' &&
      error.error !== null &&
      'message' in error.error &&
      typeof error.error.message === 'string' &&
      error.error.message.trim().length > 0
    ) {
      return error.error.message;
    }

    return `${fallback} (HTTP ${error.status || 'unknown'})`;
  }

  private extractMessageFromJson(value: string): string | null {
    try {
      const parsed = JSON.parse(value);
      if (
        typeof parsed === 'object' &&
        parsed !== null &&
        'message' in parsed &&
        typeof parsed.message === 'string' &&
        parsed.message.trim().length > 0
      ) {
        return parsed.message;
      }
    } catch {
      return null;
    }
    return null;
  }
}
