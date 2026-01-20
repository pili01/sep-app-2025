import { Component, Input, OnInit, signal, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MerchantService } from '../../service/merchants.service';
import { Subscription } from '../../models/payment_method.model';

@Component({
  selector: 'app-merchant-subs',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './merchant-subs.component.html',
  styleUrl: './merchant-subs.component.scss'
})
export class MerchantSubs implements OnInit {
  @Input({ required: true }) merchantId!: number;
  @Output() onAddNew = new EventEmitter<void>();
  
  subscriptions = signal<Subscription[]>([]);
  isLoading = signal(false);

  constructor(private merchantService: MerchantService) {}

  ngOnInit() {
    this.loadSubscriptions();
  }

  loadSubscriptions() {
    this.isLoading.set(true);

    this.merchantService.getSubscriptions(this.merchantId).subscribe({
      next: (data) => {
        this.subscriptions.set(data);
        this.isLoading.set(false);
      },
      error: () => this.isLoading.set(false)
    });
}

  toggleStatus(sub: any) {
    const newStatus = !sub.enabled;
    this.merchantService.updateSubscription(sub.id, { enabled: newStatus }).subscribe({
      next: (updated) => {
        this.subscriptions.update(list => 
          list.map(s => s.id === updated.id ? updated : s)
        );
      },
      error: (err) => {
        // This catches your Java IllegalStateException (last active method)
        alert(err.error?.error || "Operation failed");
      }
    });
  }
}