import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MerchantService } from '../../service/merchants.service';

@Component({
  selector: 'app-merchants',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './merchants.html',
  styleUrl: './merchants.scss'
})
export class Merchants implements OnInit {
  merchants = signal<any[]>([]);
  selectedMerchant = signal<any | null>(null);
  
  // New UI State Signals
  isModalOpen = signal(false);
  modalMode = signal<'ADD' | 'DETAILS'>('ADD');
  showSensitive = signal(false);

  newMerchant = { name: '', successUrl: '', failedUrl: '', errorUrl: '' };

  constructor(private merchantService: MerchantService) {}

  ngOnInit() { this.loadMerchants(); }

  loadMerchants() {
    this.merchantService.getAll().subscribe(data => this.merchants.set(data));
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
        this.newMerchant = { name: '', successUrl: '', failedUrl: '', errorUrl: '' };
      },
      error: (err) => alert("Error: " + err.error)
    });
  }
}