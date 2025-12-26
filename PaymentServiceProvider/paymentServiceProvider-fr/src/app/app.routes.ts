import { Routes } from '@angular/router';
import { Login } from './pages/login/login';
import { Home } from './pages/home/home';
import { authGuard } from '../service/auth.guard'; // Assume you created this from my previous msg

export const routes: Routes = [
  { path: 'login', component: Login },
  { 
    path: 'home', 
    component: Home,
    canActivate: [authGuard] // Guard protects this point
  },
  { path: '', redirectTo: 'login', pathMatch: 'full' },
  { path: '**', redirectTo: 'login' }
];