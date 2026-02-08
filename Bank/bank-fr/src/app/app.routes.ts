import { Routes } from '@angular/router';
import { Home } from './components/home/home';
import { PayWithQr } from './components/pay-with-qr/pay-with-qr';
import { PaymentQrCode } from './components/payment-qr-code/payment-qr-code';
import { PaymentFormComponent } from './components/payment-form/payment-form.component';
import { authGuard } from './guards/auth.guard';
import { LoginComponent } from './components/login/login.component';
import { RegisterComponent } from './components/register/register.component';
import { ProfileComponent } from './components/profile/profile.component';

export const routes: Routes = [
  {
    path: 'payment/:paymentId', component: PaymentFormComponent
  },
  { path: 'login', component: LoginComponent },
  { path: 'register', component: RegisterComponent },
  { path: 'payment-qr/:paymentId', component: PaymentQrCode },
  {
    path: 'profile', component: ProfileComponent,
    canActivate: [authGuard],
    data: { roles: ['CLIENT'] }
  },
  {
    path: 'pay-with-qr', component: PayWithQr,
    canActivate: [authGuard],
    data: { roles: ['CLIENT'] }
  },
  {
    path: 'home', component: Home,
    canActivate: [authGuard],
    data: { roles: ['CLIENT'] }
  },
  { path: '', redirectTo: '/home', pathMatch: 'full' },
];
