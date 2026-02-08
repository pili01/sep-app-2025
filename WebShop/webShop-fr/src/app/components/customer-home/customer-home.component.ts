import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { VehicleService, Vehicle } from '../../services/vehicle.service';
import { EquipmentService, Equipment } from '../../services/equipment.service';
import { InsuranceService, Insurance } from '../../services/insurance.service';
import { RentalService, RentalRequest } from '../../services/rental.service';
import { AuthService } from '../../services/auth.service';
import { Router } from '@angular/router';
import { forkJoin } from 'rxjs';

@Component({
  selector: 'app-customer-home',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './customer-home.component.html',
  styleUrl: './customer-home.component.scss'
})
export class CustomerHomeComponent implements OnInit {
  vehicles = signal<Vehicle[]>([]);
  isLoading = signal(false);
  errorMessage = signal('');

  // Rental modal
  showRentalModal = signal(false);
  selectedVehicle = signal<Vehicle | null>(null);
  equipmentList = signal<Equipment[]>([]);
  insuranceList = signal<Insurance[]>([]);
  rentalForm: FormGroup;
  selectedEquipments= signal<Equipment[]>([]);
  selectedInsuranceId = signal<number | null>(null);
  isLoadingRental = signal(false);
  rentalErrorMessage = signal('');
  rentalSuccessMessage = signal('');

  constructor(
    private vehicleService: VehicleService,
    private equipmentService: EquipmentService,
    private insuranceService: InsuranceService,
    private rentalService: RentalService,
    private authService: AuthService,
    private router: Router,
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
    this.isLoading.set(true);
    this.errorMessage.set('');
    
    this.vehicleService.getAllVehicles().subscribe({
      next: (vehicles) => {
        this.vehicles.set(vehicles || []);
        this.isLoading.set(false);
      },
      error: (error) => {
        console.error('Error loading vehicles:', error);
        this.errorMessage.set(error.message || 'Greška pri učitavanju vozila.');
        this.isLoading.set(false);
      }
    });
  }

  rentVehicle(vehicle: Vehicle): void {
    this.selectedVehicle.set(vehicle);
    this.selectedEquipments.set([]);
    this.selectedInsuranceId.set(null);
    this.rentalForm.reset();
    this.rentalErrorMessage.set('');
    this.rentalSuccessMessage.set('');
    this.loadEquipmentAndInsurance();
    this.showRentalModal.set(true);
  }

  closeRentalModal(): void {
    this.showRentalModal.set(false);
    this.selectedVehicle.set(null);
    this.selectedEquipments.set([]);
    this.selectedInsuranceId.set(null);
    this.rentalForm.reset();
    this.rentalErrorMessage.set('');
    this.rentalSuccessMessage.set('');
  }

  loadEquipmentAndInsurance(): void {
    forkJoin({
      equipment: this.equipmentService.getAllEquipment(),
      insurances: this.insuranceService.getAllInsurances()
    }).subscribe({
      next: ({ equipment, insurances }) => {
        this.equipmentList.set(equipment || []);
        this.insuranceList.set(insurances || []);
      },
      error: (error) => {
        console.error('Error loading equipment/insurance:', error);
        this.rentalErrorMessage.set(error.message || 'Greška pri učitavanju opreme i osiguranja.');
      }
    });
  }

  toggleEquipment(equipment: Equipment): void {
    if (!this.selectedEquipments().includes(equipment)) {
      this.selectedEquipments.set([...this.selectedEquipments(), equipment]);
    } else {
      this.selectedEquipments.set(this.selectedEquipments().filter(e => e !== equipment));
    }
    this.calculateTotalPrice();
  }

  selectInsurance(insuranceId: number): void {
    this.selectedInsuranceId.set(insuranceId);
    this.rentalForm.patchValue({ insuranceId });
    this.calculateTotalPrice();
  }

  isEquipmentSelected(equipment: Equipment): boolean {
    return this.selectedEquipments().includes(equipment);
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

    const vehiclePrice = (this.selectedVehicle()?.pricePerDay || 0) * days;
    const selectedInsurance = this.insuranceList().find(i => i.id === this.selectedInsuranceId());
    const insurancePrice = selectedInsurance ? selectedInsurance.pricePerDay * days : 0;

    const equipmentPrice = this.selectedEquipments().reduce((sum, equipment) => {
      return sum + (equipment.pricePerDay * days);
    }, 0);

    return vehiclePrice + insurancePrice + equipmentPrice;
  }

  getPricePerDay(): number {
    if (!this.selectedVehicle || !this.selectedInsuranceId) {
      return 0;
    }

    const vehiclePrice = this.selectedVehicle()?.pricePerDay;
    const selectedInsurance = this.insuranceList().find(i => i.id === this.selectedInsuranceId());
    const insurancePrice = selectedInsurance ? selectedInsurance.pricePerDay : 0;

    const equipmentPrice = this.selectedEquipments().reduce((sum, equipment) => {
      return sum + (equipment.pricePerDay);
    }, 0);

    return (vehiclePrice || 0) + insurancePrice + equipmentPrice;
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
    if (this.rentalForm.valid && this.selectedVehicle() && this.selectedInsuranceId()) {
      this.isLoadingRental.set(true);
      this.rentalErrorMessage.set('');
      this.rentalSuccessMessage.set('');

      const startDate = new Date(this.rentalForm.get('startDate')?.value);
      const endDate = new Date(this.rentalForm.get('endDate')?.value);

      // Format dates as ISO string for backend
      const rentalRequest: RentalRequest = {
        vehicleId: this.selectedVehicle()?.id!,
        startDate: startDate.toISOString(),
        endDate: endDate.toISOString(),
        insuranceId: this.selectedInsuranceId()!,
        equipment: this.selectedEquipments()
      };

      this.rentalService.createRental(rentalRequest).subscribe({
        next: (response) => {
          // Response is payment URL - redirect to PSP
          this.rentalSuccessMessage.set('Iznajmljivanje je uspešno kreirano!');
          this.isLoadingRental.set(false);
          setTimeout(() => {
            this.closeRentalModal();
            this.router.navigate(['/my-rentals']);
          }, 2000);
        },
        error: (error) => {
          this.isLoadingRental.set(false);
          this.rentalErrorMessage.set(error.message || error.error?.message || error.error || 'Greška pri kreiranju iznajmljivanja.');
          console.error('Rental error:', error);
        }
      });
    } else {
      this.markFormGroupTouched(this.rentalForm);
      if (!this.selectedInsuranceId()) {
        this.rentalErrorMessage.set('Morate izabrati osiguranje.');
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

