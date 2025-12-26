import { Component, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../service/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './login.html',
  styleUrl: './login.scss'
})
export class Login {
  credentials = {
    email: '',
    password: ''
  };

  errorMessage = signal<string | null>(null);

  constructor(
    private authService: AuthService, 
    private router: Router
  ) {}

  handleLogin() {
    this.errorMessage.set(null);

    this.authService.login(this.credentials).subscribe({
      next: (response) => {
        console.log('Login successful');
        this.router.navigate(['/home']);
      },
      error: (err) => {
        console.error(err);
        this.errorMessage.set('Invalid email or password. Please try again.');
      }
    });
  }
}