import { Component, Input, Output, EventEmitter, OnInit, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MerchantService } from '../../service/merchants.service';
import { PaymentMethod } from '../../models/payment_method.model';

@Component({
  selector: 'app-add-sub',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './add-merchant-sub.component.html',
  styleUrl: './add-merchant-sub.component.scss'
})
export class AddSub implements OnInit {
  @Input({ required: true }) merchantId!: number;

  @Output() onSaved = new EventEmitter<void>();
  @Output() onCancel = new EventEmitter<void>();

  isLoading = signal(true);
  paymentMethods = signal<PaymentMethod[]>([]);

  selectedPaymentMethod = { id: 0, name: '', paymentMethodCode: '', active: null, merchantAccountNumber: '', configJson: '{}'};

  constructor(private merchantService: MerchantService) { }

  ngOnInit() {
    this.loadCurrentState();
  }

  loadCurrentState() {
  this.isLoading.set(true);

  this.merchantService
    .getAvailableSubscriptions(this.merchantId)
    .subscribe({
      next: (methods) => {
        this.paymentMethods.set(methods);

        if (methods.length > 0) {
          this.selectedPaymentMethod = methods[0];
        }

        this.isLoading.set(false);
      },
      error: () => this.isLoading.set(false)
    });
}

  onSubmit() {
    if (this.selectedPaymentMethod.paymentMethodCode == 'BANK_CARD' && !this.selectedPaymentMethod.merchantAccountNumber) {
      alert("Merchant account number is required for BANK_CARD");
      return;
    }
    this.merchantService.createSubscription(this.merchantId, this.selectedPaymentMethod).subscribe({
      next: () => this.onSaved.emit(),
      error: (err) => alert(err.error?.error || "Creation failed")
    });
  }

  comparePaymentMethods = (a: PaymentMethod, b: PaymentMethod) =>
  a && b ? a.id === b.id : a === b;
}