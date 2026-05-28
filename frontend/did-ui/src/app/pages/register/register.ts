import { NgIf } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { Wallet } from 'ethers';
import { finalize } from 'rxjs';

import { Api, DidDocument, RegisterDidRequest } from '../../core/api';
import { formatHttpErrorMessage } from '../../core/http-error';

interface GeneratedWalletSnapshot {
  did: string;
  address: string;
  publicKey: string;
  privateKey: string;
}

@Component({
  selector: 'app-register',
  imports: [
    MatCardModule,
    MatButtonModule,
    MatCheckboxModule,
    MatFormFieldModule,
    MatInputModule,
    ReactiveFormsModule,
    NgIf
  ],
  templateUrl: './register.html',
  styleUrl: './register.scss'
})
export class Register {
  private readonly formBuilder = inject(FormBuilder);
  private readonly api = inject(Api);

  readonly registerForm = this.formBuilder.nonNullable.group({
    did: ['', [Validators.required]],
    publicKey: ['', [Validators.required]],
    backupConfirmed: [false, [Validators.requiredTrue]]
  });

  isSubmitting = false;
  isRevoking = false;
  successMessage: string | null = null;
  errorMessage: string | null = null;
  registeredDid: DidDocument | null = null;
  generatedPrivateKey: string | null = null;
  private generatedWallet: GeneratedWalletSnapshot | null = null;

  generateKeyPair(): void {
    const wallet = Wallet.createRandom();
    const did = `did:ethr:${wallet.address.toLowerCase()}`;

    this.registerForm.setValue({
      did,
      publicKey: wallet.signingKey.publicKey,
      backupConfirmed: false
    });
    this.generatedPrivateKey = wallet.privateKey;
    this.generatedWallet = {
      did,
      address: wallet.address.toLowerCase(),
      publicKey: wallet.signingKey.publicKey,
      privateKey: wallet.privateKey
    };

    this.successMessage = null;
    this.errorMessage = null;
    this.registeredDid = null;
  }

  submitRegistration(): void {
    if (this.registerForm.invalid) {
      this.registerForm.markAllAsTouched();
      return;
    }

    this.successMessage = null;
    this.errorMessage = null;
    this.registeredDid = null;

    if (this.generatedWallet === null) {
      this.errorMessage = 'Generate a key pair before registration.';
      return;
    }

    const formValue = this.registerForm.getRawValue();
    if (
      formValue.did !== this.generatedWallet.did ||
      formValue.publicKey !== this.generatedWallet.publicKey
    ) {
      this.errorMessage = 'DID and public key must match the generated wallet.';
      return;
    }

    this.isSubmitting = true;
    const payload: RegisterDidRequest = {
      did: this.generatedWallet.did,
      publicKey: this.generatedWallet.publicKey
    };
    this.api
      .registerDid(payload)
      .pipe(finalize(() => (this.isSubmitting = false)))
      .subscribe({
        next: (document) => {
          this.registeredDid = document;
          this.successMessage = `DID ${document.did} has been registered.`;
        },
        error: (error: HttpErrorResponse) => {
          this.errorMessage = formatHttpErrorMessage(error, 'Registration failed');
        }
      });
  }

  revokeRegisteredDid(): void {
    if (this.registeredDid === null || this.registeredDid.status === 'REVOKED') {
      return;
    }

    const shouldRevoke = window.confirm(
      `Revoke DID ${this.registeredDid.did}? This action will block JWT issuance for this DID.`
    );
    if (!shouldRevoke) {
      return;
    }

    this.errorMessage = null;
    this.successMessage = null;
    this.isRevoking = true;
    const didDocument = this.registeredDid;
    const didToRevoke = didDocument.did;

    this.api
      .revokeDid(didToRevoke)
      .pipe(finalize(() => (this.isRevoking = false)))
      .subscribe({
        next: () => {
          const revokedAt = new Date().toISOString();
          this.registeredDid = {
            ...didDocument,
            status: 'REVOKED',
            updatedAt: revokedAt
          };
          this.successMessage = `DID ${didToRevoke} has been revoked.`;
        },
        error: (error: HttpErrorResponse) => {
          this.errorMessage = formatHttpErrorMessage(error, 'DID revocation failed');
        }
      });
  }
}
