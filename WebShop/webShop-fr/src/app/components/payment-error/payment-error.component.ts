import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, ActivatedRoute } from '@angular/router';

@Component({
  selector: 'app-payment-error',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './payment-error.component.html',
  styleUrl: './payment-error.component.scss',
})
export class PaymentErrorComponent implements OnInit {
  countdown = signal(15);
  errorType = signal<string>('unknown');
  private intervalId: any;

  constructor(
    private router: Router,
    private route: ActivatedRoute,
  ) {}

  ngOnInit(): void {
    // Pročitaj tip greške iz query parametra
    this.route.queryParams.subscribe((params) => {
      this.errorType.set(params['error'] || 'unknown');
    });

    this.startCountdown();
  }

  startCountdown(): void {
    this.intervalId = setInterval(() => {
      const current = this.countdown();
      if (current > 0) {
        this.countdown.set(current - 1);
      } else {
        this.navigateToRentals();
      }
    }, 1000);
  }

  navigateToRentals(): void {
    if (this.intervalId) {
      clearInterval(this.intervalId);
    }
    this.router.navigate(['/my-rentals']);
  }

  getErrorMessage(): string {
    const type = this.errorType();
    switch (type) {
      case 'capture_failed':
        return 'PayPal plaćanje nije uspelo. Transakcija je odbijena.';
      case 'capture_exception':
        return 'Došlo je do tehničke greške tokom obrade plaćanja.';
      default:
        return 'Došlo je do neočekivane greške prilikom plaćanja.';
    }
  }

  ngOnDestroy(): void {
    if (this.intervalId) {
      clearInterval(this.intervalId);
    }
  }
}
