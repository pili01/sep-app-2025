import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { PaymentService } from '../../services/payment.service';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-payment-callback',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './payment-callback.component.html',
  styleUrl: './payment-callback.component.scss',
})
export class PaymentCallbackComponent implements OnInit {
  isLoading = signal(true);
  paymentStatus = signal<string | null>(null);
  errorMessage = signal<string | null>(null);
  transactionId = signal<string | null>(null);
  rentalId = signal<number | null>(null);

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private paymentService: PaymentService,
    private authService: AuthService,
  ) {}

  ngOnInit(): void {
    if (!this.authService.isAuthenticated()) {
      this.router.navigate(['/login']);
      return;
    }

    // Čitam transactionId iz query parametra
    this.route.queryParams.subscribe((params) => {
      const transactionId = params['transactionId'];

      if (!transactionId) {
        this.errorMessage.set('Transaction ID nije pronađen u URL-u.');
        this.isLoading.set(false);
        return;
      }

      this.transactionId.set(transactionId);
      this.checkPaymentStatus(transactionId);
    });
  }

  checkPaymentStatus(transactionId: string): void {
    this.isLoading.set(true);
    this.errorMessage.set(null);
    this.paymentStatus.set(null);

    this.paymentService.getPaymentStatus(transactionId).subscribe({
      next: (response) => {
        this.isLoading.set(false);
        this.paymentStatus.set(response.status);
        this.rentalId.set(response.rentalId);

        // Ako je plaćanje uspešno, redirektujem na my-rentals nakon 10 sekundi
        // Korisnik može i ranije da klikne dugme
        if (response.status === 'COMPLETED') {
          setTimeout(() => {
            this.router.navigate(['/my-rentals']);
          }, 10000); // 10 sekundi umesto 3
        }
      },
      error: (error) => {
        this.isLoading.set(false);
        console.error('Error checking payment status:', error);
        this.errorMessage.set(error.error?.error || 'Greška pri proveri statusa plaćanja.');
      },
    });
  }

  goToRentals(): void {
    this.router.navigate(['/my-rentals']);
  }
}
