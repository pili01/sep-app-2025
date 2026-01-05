import { Component, signal } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../services/auth.service';
import { finalize } from 'rxjs/operators';

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
            this.errorMessage.set(error.error?.message || error.message || 'Greška pri prijavljivanju. Pokušajte ponovo.');
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
}

