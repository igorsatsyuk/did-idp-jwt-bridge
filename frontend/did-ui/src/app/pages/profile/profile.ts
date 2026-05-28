import { JsonPipe, NgIf } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { Router } from '@angular/router';
import { finalize } from 'rxjs';

import { Api, ProfileResponse } from '../../core/api';
import { readAccessToken } from '../../core/auth-session';
import { formatHttpErrorMessage } from '../../core/http-error';

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
      this.navigateToAuth();
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
            this.navigateToAuth();
            return;
          }
          this.errorMessage = formatHttpErrorMessage(error, 'Could not load protected profile');
        }
      });
  }

  private navigateToAuth(): void {
    this.router.navigate(['/auth']).catch((error: unknown) => {
      const details =
        error instanceof Error && error.message.trim().length > 0 ? ` ${error.message}` : '';
      this.errorMessage = `Failed to navigate to auth flow.${details}`;
    });
  }
}
