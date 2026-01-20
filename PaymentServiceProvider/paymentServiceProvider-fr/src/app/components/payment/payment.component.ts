import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { PaymentService } from '../../service/payment.service';
import { MerchantService } from '../../service/merchants.service';
import { Subscription } from '../../models/payment_method.model';

@Component({
  selector: 'app-payment',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './payment.component.html',
  styleUrl: './payment.component.scss'
})
export class PaymentComponent implements OnInit {
  merchantId!: number;
  transactionId: string | null = null;
  isLoading = signal(false);
  paymentMethods = signal<Subscription[]>([]);

  constructor(
    private route: ActivatedRoute,
    private paymentService: PaymentService,
    private router: Router,
    private merchantService: MerchantService
  ) { }

  ngOnInit(): void {
    const merchantIdParam = this.route.snapshot.paramMap.get('merchantId');

    if (!merchantIdParam || isNaN(Number(merchantIdParam))) {
      console.error('Invalid merchantId in route');
      return;
    }

    this.merchantId = Number(merchantIdParam);
    this.transactionId =
    this.route.snapshot.queryParamMap.get('transactionId');

  this.loadAvailablePaymentMethods();
}

  loadAvailablePaymentMethods(): void {
    this.isLoading.set(true);
    this.merchantService.getSubscriptions(this.merchantId).subscribe({
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

  selectedPaymentMethod(sub: Subscription): void {
    this.paymentService.initiatePayment(this.transactionId!, sub.id).subscribe({
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
