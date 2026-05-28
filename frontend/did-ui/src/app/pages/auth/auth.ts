import { Component } from '@angular/core';
import { MatCardModule } from '@angular/material/card';

@Component({
  selector: 'app-auth',
  imports: [MatCardModule],
  templateUrl: './auth.html',
  styleUrl: './auth.scss'
})
export class Auth {}
