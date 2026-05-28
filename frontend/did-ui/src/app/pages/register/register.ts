import { NgIf } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { Wallet } from 'ethers';
import { finalize } from 'rxjs';

import { Api, DidDocument, RegisterDidRequest } from '../../core/api';

@Component({
  selector: 'app-register',
  imports: [MatCardModule, MatButtonModule, MatFormFieldModule, MatInputModule, ReactiveFormsModule, NgIf],
  templateUrl: './register.html',
  styleUrl: './register.scss'
})
export class Register {
  private readonly formBuilder = inject(FormBuilder);
  private readonly api = inject(Api);

  readonly registerForm = this.formBuilder.nonNullable.group({
    did: ['', [Validators.required]],
    publicKey: ['', [Validators.required]]
  });

  isSubmitting = false;
  successMessage: string | null = null;
  errorMessage: string | null = null;
  registeredDid: DidDocument | null = null;

  generateKeyPair(): void {
    const wallet = Wallet.createRandom();
    const did = `did:ethr:${wallet.address.toLowerCase()}`;

    this.registerForm.setValue({
      did,
      publicKey: wallet.signingKey.publicKey
    });

    this.successMessage = null;
    this.errorMessage = null;
    this.registeredDid = null;
  }

  submitRegistration(): void {
    if (this.registerForm.invalid) {
      this.registerForm.markAllAsTouched();
      return;
    }

    this.isSubmitting = true;
    this.successMessage = null;
    this.errorMessage = null;
    this.registeredDid = null;

    const payload: RegisterDidRequest = this.registerForm.getRawValue();
    this.api
      .registerDid(payload)
      .pipe(finalize(() => (this.isSubmitting = false)))
      .subscribe({
        next: (document) => {
          this.registeredDid = document;
          this.successMessage = `DID ${document.did} has been registered.`;
        },
        error: (error: HttpErrorResponse) => {
          this.errorMessage = this.formatErrorMessage(error);
        }
      });
  }

  private formatErrorMessage(error: HttpErrorResponse): string {
    if (typeof error.error === 'string' && error.error.trim().length > 0) {
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

    return `Registration failed (HTTP ${error.status || 'unknown'})`;
  }
}
