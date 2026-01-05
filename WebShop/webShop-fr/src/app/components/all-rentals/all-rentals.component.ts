import { Component, OnInit, signal } from '@angular/core';
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
  rentals = signal<Rental[]>([]);
  isLoading = signal(false);
  errorMessage = signal('');
  successMessage = signal('');

  constructor(
    private rentalService: RentalService,
    private authService: AuthService,
    private router: Router,
  ) {}

  ngOnInit(): void {
    if (!this.authService.isAuthor()) {
      this.router.navigate(['/customer-home']);
      return;
    }
    this.loadRentals();
  }

  loadRentals(): void {
    this.isLoading.set(true);
    this.errorMessage.set('');

    this.rentalService.getAllRentals().subscribe({
      next: (rentals) => {
        // Convert Set to Array if needed
        this.rentals.set(Array.isArray(rentals) ? rentals : Array.from(rentals as any));
        this.isLoading.set(false);
      },
      error: (error) => {
        console.error('Error loading rentals:', error);
        this.errorMessage.set(error.error?.message || 'Greška pri učitavanju iznajmljivanja.');
        this.isLoading.set(false);
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
      this.isLoading.set(true);
      this.errorMessage.set('');
      this.successMessage.set('');

      this.rentalService.cancelRental(rental.id).subscribe({
        next: (response) => {
          this.successMessage.set('Iznajmljivanje je uspešno otkazano!');
          this.loadRentals();
          setTimeout(() => {
            this.successMessage.set('');
          }, 3000);
        },
        error: (error) => {
          this.isLoading.set(false);
          this.errorMessage.set(error.error?.message || error.error || 'Greška pri otkazivanju iznajmljivanja.');
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
    if (user.name && user.surname) {
      return `${user.name} ${user.surname}`;
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

