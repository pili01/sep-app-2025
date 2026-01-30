import { Routes } from '@angular/router';
import { Login } from './pages/login/login';
import { Home } from './pages/home/home';
import { authGuard } from './service/auth.guard'; // Assume you created this from my previous msg
import { Merchants } from './pages/merchants/merchants';
import { PaymentComponent } from './components/payment/payment.component';
import { PaymentMethods } from './pages/payment-methods/payment-methods';

export const routes: Routes = [
  { path: 'login', component: Login },
  { 
    path: 'home', 
    component: Home,
    canActivate: [authGuard]
  },
  { 
    path: 'merchants', 
    component: Merchants, 
    canActivate: [authGuard] 
  },
  { 
    path: 'payment-methods', 
    component: PaymentMethods, 
    canActivate: [authGuard] 
  },
  { path: 'payment/:merchantId', component: PaymentComponent },
  { path: '', redirectTo: 'login', pathMatch: 'full' },
  { path: '**', redirectTo: 'login' }
];