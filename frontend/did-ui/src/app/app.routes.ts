import { Routes } from '@angular/router';
import { Auth } from './pages/auth/auth';
import { Profile } from './pages/profile/profile';
import { Register } from './pages/register/register';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'register' },
  { path: 'register', component: Register },
  { path: 'auth', component: Auth },
  { path: 'profile', component: Profile },
  { path: '**', redirectTo: 'register' }
];
