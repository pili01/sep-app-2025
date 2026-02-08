import { Component, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../service/auth.service';

interface UserLoginAttempt {
  email: string;
  failedAttempts: number;
  lastAttemptAt: Date;
  blockedUntil?: Date;
}

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './login.html',
  styleUrl: './login.scss',
})
export class Login {
  credentials = {
    email: '',
    password: '',
  };

  errorMessage = signal<string | null>(null);
  loginAttempts = signal<UserLoginAttempt[]>([]);

  constructor(
    private authService: AuthService,
    private router: Router,
  ) {}

  handleLogin() {
    this.errorMessage.set(null);

    const email = this.credentials.email.trim();
    const attempt = this.getAttempt(email);

    if (attempt && this.isBlocked(attempt)) {
      const minutes = this.getRemainingMinutes(attempt);
      this.errorMessage.set(
        `Account is temporarily locked. Try again in ${minutes} minute(s).`
      );
      return;
    }

    this.authService.login(this.credentials).subscribe({
      next: (response) => {
        const email = this.credentials.email.trim();

        this.loginAttempts.update(attempts =>
          attempts.filter(a => a.email !== email)
        );
        console.log('Login successful');
        this.router.navigate(['/home']);
      },
      error: (err) => {
        console.error(err);

        const email = this.credentials.email.trim();
        const now = new Date();

        this.loginAttempts.update(attempts => {
          const existing = attempts.find(a => a.email === email);

          if (!existing) {
            attempts.push({
              email,
              failedAttempts: 1,
              lastAttemptAt: now,
            });
          } else {
            existing.failedAttempts++;
            existing.lastAttemptAt = now;

            if (existing.failedAttempts >= 5) {
              existing.blockedUntil = new Date(
                now.getTime() + 60 * 60 * 1000 
              );
            }
          }

          return [...attempts];
        });

        const updated = this.getAttempt(email);

        if (updated?.blockedUntil) {
          this.errorMessage.set(
            'Too many failed attempts. Account locked for 1 hour.'
          );
        } else {
          this.errorMessage.set(
            `Invalid credentials. Attempt ${updated?.failedAttempts}/5.`
          );
        }
      },
    });
  }

  private getAttempt(email: string): UserLoginAttempt | undefined {
    return this.loginAttempts().find(a => a.email === email);
  }

  private isBlocked(attempt: UserLoginAttempt): boolean {
    return !!attempt.blockedUntil && new Date() < attempt.blockedUntil;
  }

  private getRemainingMinutes(attempt: UserLoginAttempt): number {
    if (!attempt.blockedUntil) return 0;
    const diff = attempt.blockedUntil.getTime() - Date.now();
    return Math.ceil(diff / 60000);
  }
}
