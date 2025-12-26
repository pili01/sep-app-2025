import { Routes } from '@angular/router';
import { PaymentFormComponent } from './components/payment-form/payment-form.component';

export const routes: Routes = [
  { path: 'payment/:paymentId', component: PaymentFormComponent },
  { path: '', redirectTo: '/payment', pathMatch: 'full' }
];
