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
  transactionId: string | null = null;
  isLoading = signal(false);
  paymentMethods = signal<any[]>([]);

  constructor(
    private route: ActivatedRoute,
    private paymentService: PaymentService,
    private router: Router
  ) { }

  ngOnInit(): void {
    this.merchantId = this.route.snapshot.paramMap.get('merchantId');
    this.transactionId = this.route.snapshot.queryParamMap.get('transactionId');
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

  selectedPaymentMethod(method: any): void {
    this.paymentService.initiatePayment(this.transactionId!, method).subscribe({
      next: (response) => {
        if (response.paymentUrl)
          window.location.href = response.paymentUrl;
        else
          alert('Payment URL not found in response');
      },
      error: (error) => {
        alert('Error initiating payment: ' + error.message);
      }
    });
  }
}
