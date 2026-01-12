import { inject } from '@angular/core';
import { Router, CanActivateFn } from '@angular/router';
import { AuthService } from '../services/auth.service';

export const authGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  const expectedRoles = route.data?.['roles'] as string[] | undefined;

  if (!authService.isAuthenticated()) {
    router.navigate(['/login']);
    return false;
  }

  const user = authService.currentUser();
  if (expectedRoles && user && expectedRoles.includes(user.userRole)) {
    return true;
  }

  if (expectedRoles && user && !expectedRoles.includes(user.userRole)) {
    // Ako korisnik nema odgovarajuću ulogu, preusmeri na odgovarajuću početnu stranicu
    if (user.userRole === 'CLIENT') {
      router.navigate(['/home']);
    } else if (user.userRole === 'CUSTOMER') {
      router.navigate(['/customer-home']);
    } else {
      router.navigate(['/login']);
    }
    return false;
  }

  router.navigate(['/login']);
  return false;
};

