import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RentalService, Rental } from '../../services/rental.service';
import { AuthService } from '../../services/auth.service';
import { Router } from '@angular/router';

@Component({
  selector: 'app-all-rentals',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './all-rentals.component.html',
  styleUrl: './all-rentals.component.scss'
})
export class AllRentalsComponent implements OnInit {
  rentals: Rental[] = [];
  isLoading = false;
  errorMessage = '';
  successMessage = '';

  constructor(
    private rentalService: RentalService,
    private authService: AuthService,
    private router: Router,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    if (!this.authService.isAuthor()) {
      this.router.navigate(['/customer-home']);
      return;
    }
    this.loadRentals();
  }

  loadRentals(): void {
    this.isLoading = true;
    this.errorMessage = '';
    this.cdr.detectChanges();

    this.rentalService.getAllRentals().subscribe({
      next: (rentals) => {
        // Convert Set to Array if needed
        this.rentals = Array.isArray(rentals) ? rentals : Array.from(rentals as any);
        this.isLoading = false;
        this.cdr.detectChanges();
      },
      error: (error) => {
        console.error('Error loading rentals:', error);
        this.errorMessage = error.error?.message || 'Greška pri učitavanju iznajmljivanja.';
        this.isLoading = false;
        this.cdr.detectChanges();
      }
    });
  }

  cancelRental(rental: Rental): void {
    if (!rental.id) return;
    
    if (rental.status !== 'DRAFT') {
      alert('Samo iznajmljivanja sa statusom DRAFT mogu biti otkazana.');
      return;
    }

    if (confirm('Da li ste sigurni da želite da otkažete ovo iznajmljivanje?')) {
      this.isLoading = true;
      this.errorMessage = '';
      this.successMessage = '';
      this.cdr.detectChanges();

      this.rentalService.cancelRental(rental.id).subscribe({
        next: (response) => {
          this.successMessage = 'Iznajmljivanje je uspešno otkazano!';
          this.loadRentals();
          setTimeout(() => {
            this.successMessage = '';
          }, 3000);
        },
        error: (error) => {
          this.isLoading = false;
          this.errorMessage = error.error?.message || error.error || 'Greška pri otkazivanju iznajmljivanja.';
          this.cdr.detectChanges();
        }
      });
    }
  }

  formatDate(dateString: string): string {
    const date = new Date(dateString);
    return date.toLocaleString('sr-RS', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit'
    });
  }

  getStatusLabel(status: string): string {
    return status === 'DRAFT' ? 'Nacrt' : 'Kupljeno';
  }

  getStatusClass(status: string): string {
    return status === 'DRAFT' ? 'status-draft' : 'status-purchased';
  }

  getUserName(user: any): string {
    if (!user) return 'Nepoznato';
    if (user.firstName && user.lastName) {
      return `${user.firstName} ${user.lastName}`;
    }
    return user.email || 'Nepoznato';
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
}

