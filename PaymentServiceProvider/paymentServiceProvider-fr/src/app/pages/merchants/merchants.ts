import { Component, OnInit, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MerchantService } from '../../service/merchants.service';
import { MerchantSubs } from '../../components/merchant-subs/merchant-subs.component';
import { AddSub } from '../../components/add-merchant-subs/add-merchant-sub.component';

@Component({
  selector: 'app-merchants',
  standalone: true,
  imports: [CommonModule, FormsModule, MerchantSubs, AddSub],
  templateUrl: './merchants.html',
  styleUrl: './merchants.scss',
})
export class Merchants implements OnInit {
  merchants = signal<any[]>([]);
  selectedMerchant = signal<any | null>(null);

  isModalOpen = signal(false);
  modalMode = signal<'ADD' | 'DETAILS' | 'SUBS' | 'ADD_SUB'>('ADD');

  modalTitle = computed(() => {
    const mode = this.modalMode();
    const merchantName = this.selectedMerchant()?.name;

    switch (mode) {
      case 'ADD':
        return 'Register New Merchant';
      case 'SUBS':
        return `Subscriptions: ${merchantName}`;
      case 'ADD_SUB':
        return `Add New Payment for ${merchantName}`;
      case 'DETAILS':
        return 'Merchant Details';
      default:
        return 'Management';
    }
  });
  showSensitive = signal(false);

  newMerchant = { name: '', webHookUrl: '', successUrl: '', failedUrl: '', errorUrl: '' };

  constructor(private merchantService: MerchantService) {}

  ngOnInit() {
    this.loadMerchants();
  }

  loadMerchants() {
    this.merchantService.getAll().subscribe((data) => this.merchants.set(data));
  }

  openAddSubscription() {
    this.modalMode.set('ADD_SUB');
  }

  openSubscriptions(m: any) {
    this.selectedMerchant.set(m);
    this.modalMode.set('SUBS');
    this.isModalOpen.set(true);
  }

  openAddModal() {
    this.modalMode.set('ADD');
    this.isModalOpen.set(true);
  }

  openDetails(m: any) {
    this.selectedMerchant.set(m);
    this.modalMode.set('DETAILS');
    this.showSensitive.set(false);
    this.isModalOpen.set(true);
  }

  closeModal() {
    this.isModalOpen.set(false);
    this.selectedMerchant.set(null);
  }

  onCreate() {
    this.merchantService.create(this.newMerchant).subscribe({
      next: () => {
        this.loadMerchants();
        this.closeModal();
        this.newMerchant = {
          name: '',
          webHookUrl: '',
          successUrl: '',
          failedUrl: '',
          errorUrl: '',
        };
      },
      error: (err) => alert('Error: ' + err.error),
    });
  }
}
