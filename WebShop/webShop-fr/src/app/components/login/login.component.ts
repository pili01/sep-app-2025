import { Component, ChangeDetectorRef } from '@angular/core';
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
  hidePassword = true;
  isLoading = false;
  errorMessage = '';

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router,
    private cdr: ChangeDetectorRef
  ) {
    this.loginForm = this.fb.group({
      email: ['', [Validators.required]],
      password: ['', [Validators.required]]
    });
  }

  togglePasswordVisibility(): void {
    this.hidePassword = !this.hidePassword;
  }

  onSubmit(): void {
    if (this.loginForm.valid) {
      this.isLoading = true;
      this.errorMessage = '';

      this.authService.login(this.loginForm.value)
        .pipe(
          finalize(() => {
            console.log('Finalize called - setting isLoading to false');
            this.isLoading = false;
            this.cdr.detectChanges();
            console.log('isLoading after finalize:', this.isLoading);
          })
        )
        .subscribe({
          next: (response) => {
            console.log("uspesno prijavljivanje", response);
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
            this.errorMessage = error.error?.message || error.message || 'Greška pri prijavljivanju. Pokušajte ponovo.';
            this.cdr.detectChanges();
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

