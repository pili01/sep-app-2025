import { Routes } from '@angular/router';
import { authorGuard } from './guards/author.guard';

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
    loadComponent: () => import('./components/profile/profile.component').then(m => m.ProfileComponent)
  },
  {
    path: 'home',
    loadComponent: () => import('./components/home/home.component').then(m => m.HomeComponent),
    canActivate: [authorGuard]
  },
  {
    path: 'equipment',
    loadComponent: () => import('./components/equipment/equipment.component').then(m => m.EquipmentComponent),
    canActivate: [authorGuard]
  },
  {
    path: 'insurance',
    loadComponent: () => import('./components/insurance/insurance.component').then(m => m.InsuranceComponent),
    canActivate: [authorGuard]
  },
  {
    path: 'customer-home',
    loadComponent: () => import('./components/customer-home/customer-home.component').then(m => m.CustomerHomeComponent)
  },
  {
    path: 'my-rentals',
    loadComponent: () => import('./components/my-rentals/my-rentals.component').then(m => m.MyRentalsComponent)
  },
  {
    path: 'all-rentals',
    loadComponent: () => import('./components/all-rentals/all-rentals.component').then(m => m.AllRentalsComponent),
    canActivate: [authorGuard]
  },
  {
    path: '',
    redirectTo: '/login',
    pathMatch: 'full'
  }
];
