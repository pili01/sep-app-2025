import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { VehicleService, Vehicle } from '../../services/vehicle.service';
import { EquipmentService, Equipment } from '../../services/equipment.service';
import { InsuranceService, Insurance } from '../../services/insurance.service';
import { RentalService, RentalRequest } from '../../services/rental.service';
import { AuthService } from '../../services/auth.service';
import { Router } from '@angular/router';

@Component({
  selector: 'app-customer-home',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './customer-home.component.html',
  styleUrl: './customer-home.component.scss'
})
export class CustomerHomeComponent implements OnInit {
  vehicles: Vehicle[] = [];
  isLoading = false;
  errorMessage = '';
  
  // Rental modal
  showRentalModal = false;
  selectedVehicle: Vehicle | null = null;
  equipmentList: Equipment[] = [];
  insuranceList: Insurance[] = [];
  rentalForm: FormGroup;
  selectedEquipmentIds: number[] = [];
  selectedInsuranceId: number | null = null;
  isLoadingRental = false;
  rentalErrorMessage = '';
  rentalSuccessMessage = '';

  constructor(
    private vehicleService: VehicleService,
    private equipmentService: EquipmentService,
    private insuranceService: InsuranceService,
    private rentalService: RentalService,
    private authService: AuthService,
    private router: Router,
    private cdr: ChangeDetectorRef,
    private fb: FormBuilder
  ) {
    this.rentalForm = this.fb.group({
      startDate: ['', [Validators.required]],
      endDate: ['', [Validators.required]],
      insuranceId: ['', [Validators.required]]
    });
  }

  ngOnInit(): void {
    // Proveri da li je korisnik customer, ako nije preusmeri
    if (!this.authService.isCustomer()) {
      this.router.navigate(['/home']);
      return;
    }
    this.loadVehicles();
  }

  loadVehicles(): void {
    this.isLoading = true;
    this.errorMessage = '';
    this.cdr.detectChanges();
    
    this.vehicleService.getAllVehicles()
      .then((vehicles) => {
        this.vehicles = vehicles || [];
        this.isLoading = false;
        this.cdr.detectChanges();
      })
      .catch((error) => {
        console.error('Error loading vehicles:', error);
        this.errorMessage = 'Greška pri učitavanju vozila.';
        this.isLoading = false;
        this.cdr.detectChanges();
      });
  }

  rentVehicle(vehicle: Vehicle): void {
    this.selectedVehicle = vehicle;
    this.selectedEquipmentIds = [];
    this.selectedInsuranceId = null;
    this.rentalForm.reset();
    this.rentalErrorMessage = '';
    this.rentalSuccessMessage = '';
    this.loadEquipmentAndInsurance();
    this.showRentalModal = true;
  }

  closeRentalModal(): void {
    this.showRentalModal = false;
    this.selectedVehicle = null;
    this.selectedEquipmentIds = [];
    this.selectedInsuranceId = null;
    this.rentalForm.reset();
    this.rentalErrorMessage = '';
    this.rentalSuccessMessage = '';
  }

  loadEquipmentAndInsurance(): void {
    Promise.all([
      this.equipmentService.getAllEquipment(),
      this.insuranceService.getAllInsurances()
    ]).then(([equipment, insurances]) => {
      this.equipmentList = equipment || [];
      this.insuranceList = insurances || [];
      this.cdr.detectChanges();
    }).catch((error) => {
      console.error('Error loading equipment/insurance:', error);
      this.rentalErrorMessage = 'Greška pri učitavanju opreme i osiguranja.';
      this.cdr.detectChanges();
    });
  }

  toggleEquipment(equipmentId: number): void {
    const index = this.selectedEquipmentIds.indexOf(equipmentId);
    if (index > -1) {
      this.selectedEquipmentIds.splice(index, 1);
    } else {
      this.selectedEquipmentIds.push(equipmentId);
    }
    this.calculateTotalPrice();
  }

  selectInsurance(insuranceId: number): void {
    this.selectedInsuranceId = insuranceId;
    this.rentalForm.patchValue({ insuranceId });
    this.calculateTotalPrice();
  }

  isEquipmentSelected(equipmentId: number): boolean {
    return this.selectedEquipmentIds.includes(equipmentId);
  }

  calculateTotalPrice(): number {
    if (!this.selectedVehicle || !this.selectedInsuranceId) {
      return 0;
    }

    const startDate = this.rentalForm.get('startDate')?.value;
    const endDate = this.rentalForm.get('endDate')?.value;

    if (!startDate || !endDate) {
      return 0;
    }

    const start = new Date(startDate);
    const end = new Date(endDate);
    const days = Math.max(1, Math.ceil((end.getTime() - start.getTime()) / (1000 * 60 * 60 * 24)));

    const vehiclePrice = this.selectedVehicle.pricePerDay * days;
    const selectedInsurance = this.insuranceList.find(i => i.id === this.selectedInsuranceId);
    const insurancePrice = selectedInsurance ? selectedInsurance.pricePerDay * days : 0;
    
    const equipmentPrice = this.selectedEquipmentIds.reduce((sum, id) => {
      const equipment = this.equipmentList.find(e => e.id === id);
      return sum + (equipment ? equipment.pricePerDay * days : 0);
    }, 0);

    return vehiclePrice + insurancePrice + equipmentPrice;
  }

  getPricePerDay(): number {
    if (!this.selectedVehicle || !this.selectedInsuranceId) {
      return 0;
    }

    const vehiclePrice = this.selectedVehicle.pricePerDay;
    const selectedInsurance = this.insuranceList.find(i => i.id === this.selectedInsuranceId);
    const insurancePrice = selectedInsurance ? selectedInsurance.pricePerDay : 0;
    
    const equipmentPrice = this.selectedEquipmentIds.reduce((sum, id) => {
      const equipment = this.equipmentList.find(e => e.id === id);
      return sum + (equipment ? equipment.pricePerDay : 0);
    }, 0);

    return vehiclePrice + insurancePrice + equipmentPrice;
  }

  getNumberOfDays(): number {
    const startDate = this.rentalForm.get('startDate')?.value;
    const endDate = this.rentalForm.get('endDate')?.value;

    if (!startDate || !endDate) {
      return 0;
    }

    const start = new Date(startDate);
    const end = new Date(endDate);
    return Math.max(1, Math.ceil((end.getTime() - start.getTime()) / (1000 * 60 * 60 * 24)));
  }

  onSubmitRental(): void {
    if (this.rentalForm.valid && this.selectedVehicle && this.selectedInsuranceId) {
      this.isLoadingRental = true;
      this.rentalErrorMessage = '';
      this.rentalSuccessMessage = '';

      const startDate = new Date(this.rentalForm.get('startDate')?.value);
      const endDate = new Date(this.rentalForm.get('endDate')?.value);

      // Format dates as ISO string for backend
      const rentalRequest: RentalRequest = {
        vehicleId: this.selectedVehicle.id!,
        startDate: startDate.toISOString(),
        endDate: endDate.toISOString(),
        insuranceId: this.selectedInsuranceId,
        equipmentIds: this.selectedEquipmentIds
      };

      this.rentalService.createRental(rentalRequest).subscribe({
        next: (response) => {
          this.rentalSuccessMessage = 'Iznajmljivanje je uspešno kreirano!';
          this.isLoadingRental = false;
          setTimeout(() => {
            this.closeRentalModal();
            this.loadVehicles(); // Refresh vehicles list
          }, 2000);
        },
        error: (error) => {
          this.isLoadingRental = false;
          this.rentalErrorMessage = error.error?.message || error.error || 'Greška pri kreiranju iznajmljivanja.';
          console.error('Rental error:', error);
        }
      });
    } else {
      this.markFormGroupTouched(this.rentalForm);
      if (!this.selectedInsuranceId) {
        this.rentalErrorMessage = 'Morate izabrati osiguranje.';
      }
    }
  }

  private markFormGroupTouched(formGroup: FormGroup): void {
    Object.keys(formGroup.controls).forEach(key => {
      const control = formGroup.get(key);
      control?.markAsTouched();
    });
  }

  get startDate() {
    return this.rentalForm.get('startDate');
  }

  get endDate() {
    return this.rentalForm.get('endDate');
  }

  get insuranceId() {
    return this.rentalForm.get('insuranceId');
  }

  isDirectImageUrl(url: string | undefined): boolean {
    if (!url) return false;
    const imageExtensions = ['.jpg', '.jpeg', '.png', '.gif', '.webp', '.svg', '.bmp'];
    const lowerUrl = url.toLowerCase();
    return imageExtensions.some(ext => lowerUrl.endsWith(ext) || lowerUrl.includes(ext + '?')) ||
           lowerUrl.includes('i.imgur.com') ||
           lowerUrl.includes('images.unsplash.com') ||
           lowerUrl.includes('cdn.pexels.com') ||
           lowerUrl.includes('images.pexels.com');
  }

  onImageError(event: any): void {
    event.target.src = '/assets/icons/rent.png';
    event.target.onerror = null;
  }

  getMinDate(): string {
    const now = new Date();
    now.setMinutes(now.getMinutes() - now.getTimezoneOffset());
    return now.toISOString().slice(0, 16);
  }

  getMinEndDate(): string {
    const startDate = this.rentalForm.get('startDate')?.value;
    if (startDate) {
      return startDate;
    }
    return this.getMinDate();
  }
}

