import { JsonPipe, NgIf } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { Wallet } from 'ethers';
import { finalize } from 'rxjs';

import { Api, AuthTokenRequest, AuthTokenResponse } from '../../core/api';
import { saveAccessToken } from '../../core/auth-session';
import { formatHttpErrorMessage } from '../../core/http-error';

@Component({
  selector: 'app-auth',
  imports: [
    MatCardModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    ReactiveFormsModule,
    NgIf,
    JsonPipe
  ],
  templateUrl: './auth.html',
  styleUrl: './auth.scss'
})
export class Auth {
  private readonly formBuilder = inject(FormBuilder);
  private readonly api = inject(Api);

  readonly authForm = this.formBuilder.nonNullable.group({
    did: ['', [Validators.required]],
    privateKey: ['', [Validators.required]]
  });

  challenge: string | null = null;
  signature: string | null = null;
  tokenResponse: AuthTokenResponse | null = null;
  jwtClaims: Record<string, unknown> | null = null;
  isFetchingChallenge = false;
  isSigningChallenge = false;
  isSubmittingToken = false;
  successMessage: string | null = null;
  errorMessage: string | null = null;

  requestChallenge(): void {
    this.isFetchingChallenge = true;
    this.errorMessage = null;
    this.successMessage = null;
    this.signature = null;
    this.tokenResponse = null;
    this.jwtClaims = null;

    this.api
      .getChallenge()
      .pipe(finalize(() => (this.isFetchingChallenge = false)))
      .subscribe({
        next: (challenge) => {
          this.challenge = challenge;
          this.successMessage = 'Challenge received. Sign it with your private key.';
        },
        error: (error: HttpErrorResponse) => {
          this.errorMessage = formatHttpErrorMessage(error, 'Could not fetch challenge');
        }
      });
  }

  signChallenge(): void {
    if (this.authForm.invalid) {
      this.authForm.markAllAsTouched();
      return;
    }

    if (!this.challenge) {
      this.errorMessage = 'Fetch challenge before signing.';
      this.successMessage = null;
      return;
    }

    const privateKey = this.authForm.controls.privateKey.value.trim();
    this.isSigningChallenge = true;
    this.errorMessage = null;
    this.successMessage = null;

    let signingPromise: Promise<string>;
    try {
      signingPromise = new Wallet(privateKey).signMessage(this.challenge);
    } catch (error: unknown) {
      this.errorMessage = this.formatRuntimeError(error, 'Failed to sign challenge');
      this.isSigningChallenge = false;
      return;
    }

    signingPromise
      .then((signature) => {
        this.signature = signature;
        this.successMessage = 'Challenge signed. Exchange signature for JWT.';
      })
      .catch((error: unknown) => {
        this.errorMessage = this.formatRuntimeError(error, 'Failed to sign challenge');
      })
      .finally(() => {
        this.isSigningChallenge = false;
      });
  }

  exchangeToken(): void {
    if (this.authForm.invalid) {
      this.authForm.markAllAsTouched();
      return;
    }

    if (!this.challenge) {
      this.errorMessage = 'Fetch challenge before requesting JWT.';
      this.successMessage = null;
      return;
    }

    if (!this.signature) {
      this.errorMessage = 'Sign challenge before requesting JWT.';
      this.successMessage = null;
      return;
    }

    this.isSubmittingToken = true;
    this.errorMessage = null;
    this.successMessage = null;

    const payload: AuthTokenRequest = {
      did: this.authForm.controls.did.value.trim(),
      challenge: this.challenge,
      signature: this.signature
    };

    this.api
      .exchangeToken(payload)
      .pipe(finalize(() => (this.isSubmittingToken = false)))
      .subscribe({
        next: (response) => {
          this.tokenResponse = response;
          saveAccessToken(response.accessToken);
          this.jwtClaims = this.decodeJwtClaims(response.accessToken);
          this.successMessage = 'JWT received and saved to session storage.';
        },
        error: (error: HttpErrorResponse) => {
          this.errorMessage = formatHttpErrorMessage(error, 'Could not exchange token');
        }
      });
  }

  private decodeJwtClaims(token: string): Record<string, unknown> | null {
    const parts = token.split('.');
    if (parts.length < 2) {
      return null;
    }

    try {
      const payload = parts[1];
      const normalizedPayload = payload.replaceAll('-', '+').replaceAll('_', '/');
      const paddedPayload = normalizedPayload.padEnd(
        normalizedPayload.length + ((4 - (normalizedPayload.length % 4)) % 4),
        '='
      );
      const parsed = JSON.parse(atob(paddedPayload));
      if (typeof parsed === 'object' && parsed !== null) {
        return parsed as Record<string, unknown>;
      }
    } catch {
      return null;
    }

    return null;
  }

  private formatRuntimeError(error: unknown, fallback: string): string {
    if (error instanceof Error && error.message.trim().length > 0) {
      return error.message;
    }
    return fallback;
  }
}
