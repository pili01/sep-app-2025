import { Routes } from '@angular/router';
import { authGuard } from './guards/auth.guard';

export const routes: Routes = [
  {
    path: 'login',
    loadComponent: () => import('./components/login/login.component').then(m => m.LoginComponent)
  },
  {
    path: 'register',
    loadComponent: () => import('./components/register/register.component').then(m => m.RegisterComponent)
  },
  {
    path: 'profile',
    loadComponent: () => import('./components/profile/profile.component').then(m => m.ProfileComponent),
    canActivate: [authGuard],
    data: { roles: ['AUTHOR', 'CUSTOMER'] }
  },
  {
    path: 'home',
    loadComponent: () => import('./components/home/home.component').then(m => m.HomeComponent),
    canActivate: [authGuard],
    data: { roles: ['AUTHOR'] }
  },
  {
    path: 'equipment',
    loadComponent: () => import('./components/equipment/equipment.component').then(m => m.EquipmentComponent),
    canActivate: [authGuard],
    data: { roles: ['AUTHOR'] }
  },
  {
    path: 'insurance',
    loadComponent: () => import('./components/insurance/insurance.component').then(m => m.InsuranceComponent),
    canActivate: [authGuard],
    data: { roles: ['AUTHOR'] }
  },
  {
    path: 'customer-home',
    loadComponent: () => import('./components/customer-home/customer-home.component').then(m => m.CustomerHomeComponent),
    canActivate: [authGuard],
    data: { roles: ['CUSTOMER'] }
  },
  {
    path: 'my-rentals',
    loadComponent: () => import('./components/my-rentals/my-rentals.component').then(m => m.MyRentalsComponent),
    canActivate: [authGuard],
    data: { roles: ['CUSTOMER'] }
  },
  {
    path: 'all-rentals',
    loadComponent: () => import('./components/all-rentals/all-rentals.component').then(m => m.AllRentalsComponent),
    canActivate: [authGuard],
    data: { roles: ['AUTHOR'] }
  },
  {
    path: '',
    redirectTo: '/login',
    pathMatch: 'full'
  }
];
