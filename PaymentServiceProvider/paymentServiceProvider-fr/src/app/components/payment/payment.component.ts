import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { PaymentService } from '../../service/payment.service';
import { MerchantService } from '../../service/merchants.service';

@Component({
  selector: 'app-payment',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './payment.component.html',
  styleUrl: './payment.component.scss'
})
export class PaymentComponent implements OnInit {
  merchantId: string | null = null;
  bankPaymentId: string | null = null;
  isLoading = signal(false);
  paymentMethods = signal<any[]>([]);

  constructor(
    private route: ActivatedRoute,
    private paymentService: PaymentService,
    private router: Router
  ) { }

  ngOnInit(): void {
    this.merchantId = this.route.snapshot.paramMap.get('merchantId');
    // this.bankPaymentId = this.route.snapshot.queryParamMap.get('bankPaymentId');
    this.loadAvailablePaymentMethods(this.merchantId!);
  }

  loadAvailablePaymentMethods(merchantId: string): void {
    this.isLoading.set(true);
    this.paymentService.getAvailablePaymentMethods(merchantId).subscribe({
      next: (response) => {
        this.paymentMethods.set(response);
        this.isLoading.set(false);
      },
      error: (error) => {
        this.isLoading.set(false);
        console.error('Error loading payment methods:', error);
      }
    });
  }

  selectBankCard(): void {
    if (this.bankPaymentId) {
      window.location.href = `https://localhost:4203/payment/${this.bankPaymentId}`;
    }
  }
}
