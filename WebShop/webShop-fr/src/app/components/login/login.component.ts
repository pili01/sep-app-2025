import { Component, signal } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../services/auth.service';
import { finalize } from 'rxjs/operators';

interface UserLoginAttempt {
  username: string;
  failedAttempts: number;
  lastAttemptAt: Date;
  blockedUntil?: Date;
}

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './login.component.html',
  styleUrl: './login.component.scss'
})
export class LoginComponent {
  loginForm: FormGroup;
  hidePassword = signal(true);
  isLoading = signal(false);
  errorMessage = signal('');
  loginAttempts = signal<UserLoginAttempt[]>([]);

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router,
  ) {
    this.loginForm = this.fb.group({
      email: ['', [Validators.required]],
      password: ['', [Validators.required]]
    });
  }

  togglePasswordVisibility(): void {
    this.hidePassword.set(!this.hidePassword());
  }

  onSubmit(): void {
    if (this.loginForm.valid) {
      this.isLoading.set(true);
      this.errorMessage.set('');

      const username = this.loginForm.value.email;
      const attempt = this.getAttempt(username);

      if (attempt && this.isBlocked(attempt)) {
        this.errorMessage.set(
          `Nalog je privremeno blokiran. Pokušajte ponovo za ${this.getRemainingBlockTime(attempt)}.`
        );
        this.isLoading.set(false);
        return;
      }

      this.authService.login(this.loginForm.value)
        .pipe(
          finalize(() => {
            console.log('Finalize called - setting isLoading to false');
            this.isLoading.set(false);
            console.log('isLoading after finalize:', this.isLoading());
          })
        )
        .subscribe({
          next: (response) => {
            console.log("uspješno prijavljivanje", response);
            const username = this.loginForm.value.email;
              this.loginAttempts.update(attempts =>
                attempts.filter(a => a.username !== username)
              );

            setTimeout(() => {
              // Preusmeri na odgovarajuću stranicu na osnovu uloge
              const user = this.authService.currentUser();
              if (user?.userRole === 'AUTHOR') {
                this.router.navigate(['/home']).catch(err => {
                  console.error('Navigation error:', err);
                });
              } else {
                this.router.navigate(['/customer-home']).catch(err => {
                  console.error('Navigation error:', err);
                });
              }
            }, 100);
          },
          error: (error) => {
            console.error('Login error:', error);

            const username = this.loginForm.value.email;
            const now = new Date();

            this.loginAttempts.update(attempts => {
              const existing = attempts.find(a => a.username === username);

              if (!existing) {
                attempts.push({
                  username,
                  failedAttempts: 1,
                  lastAttemptAt: now
                });
              } else {
                existing.failedAttempts++;
                existing.lastAttemptAt = now;

                if (existing.failedAttempts >= 5) {
                  existing.blockedUntil = new Date(now.getTime() + 60 * 60 * 1000); // 1h
                }
              }

              return [...attempts];
            });

            const updated = this.getAttempt(username);

            if (updated?.blockedUntil) {
              this.errorMessage.set(
                `Previše neuspešnih pokušaja. Nalog je blokiran na 1 sat.`
              );
            } else {
              this.errorMessage.set(
                `Pogrešni kredencijali. Pokušaj ${updated?.failedAttempts}/5.`
              );
            }
          }
                  });
    } else {
      this.markFormGroupTouched(this.loginForm);
    }
  }

  private markFormGroupTouched(formGroup: FormGroup): void {
    Object.keys(formGroup.controls).forEach(key => {
      const control = formGroup.get(key);
      control?.markAsTouched();
    });
  }

  get email() {
    return this.loginForm.get('email');
  }

  get password() {
    return this.loginForm.get('password');
  }

  private getAttempt(username: string): UserLoginAttempt | undefined {
    return this.loginAttempts().find(a => a.username === username);
  }

  private isBlocked(attempt: UserLoginAttempt): boolean {
    return !!attempt.blockedUntil && new Date() < attempt.blockedUntil;
  }

  private getRemainingBlockTime(attempt: UserLoginAttempt): string {
    if (!attempt.blockedUntil) return '';

    const diffMs = attempt.blockedUntil.getTime() - Date.now();
    const minutes = Math.ceil(diffMs / 60000);

    return `${minutes} min`;
  }
}

