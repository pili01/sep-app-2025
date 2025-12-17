import { inject } from '@angular/core';
import { Router, CanActivateFn } from '@angular/router';
import { AuthService } from '../services/auth.service';

export const authorGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (!authService.isAuthenticated()) {
    router.navigate(['/login']);
    return false;
  }

  const user = authService.currentUser();
  if (user && user.userRole === 'AUTHOR') {
    return true;
  }

  // Ako nije AUTHOR, preusmeri na customer home
  router.navigate(['/customer-home']);
  return false;
};

