import { Component, Input, Output, EventEmitter, OnInit, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MerchantService } from '../../service/merchants.service';

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
  existingMethodCodes = signal<string[]>([]);
  
  allPossibleCodes = signal<string[]>(['BANK_CARD', 'BANK_QR', 'PAYPAL', 'CRYPTO_BTC']);

  availableCodes = computed(() => {
    return this.allPossibleCodes().filter(code => !this.existingMethodCodes().includes(code));
  });

  newSub = { paymentMethodCode: '', configJson: '{}' };

  constructor(private merchantService: MerchantService) {}

  ngOnInit() {
    this.loadCurrentState();
  }

  loadCurrentState() {
    this.isLoading.set(true);
    this.merchantService.getSubscriptions(this.merchantId).subscribe({
      next: (subs) => {
        this.existingMethodCodes.set(subs.map(s => s.paymentMethodCode));
        
        if (this.availableCodes().length > 0) {
          this.newSub.paymentMethodCode = this.availableCodes()[0];
        }
        this.isLoading.set(false);
      },
      error: () => this.isLoading.set(false)
    });
  }

  onSubmit() {
    this.merchantService.createSubscription(this.merchantId, this.newSub).subscribe({
      next: () => this.onSaved.emit(),
      error: (err) => alert(err.error?.error || "Creation failed")
    });
  }
}