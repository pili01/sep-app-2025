import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { catchError, throwError } from 'rxjs';

export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      let errorMessage = 'Došlo je do greške.';

      if (error.error instanceof ErrorEvent) {
        // Client-side error
        errorMessage = `Greška: ${error.error.message}`;
      } else {
        // Server-side error
        if (error.status === 0) {
          errorMessage = 'Nije moguće povezati se sa serverom. Provjerite internet konekciju.';
        } else if (error.status === 400) {
          errorMessage = error.error?.message || error.error || 'Neispravni podaci.';
        } else if (error.status === 401) {
          errorMessage = 'Neautorizovan pristup. Molimo prijavite se.';
        } else if (error.status === 403) {
          errorMessage = 'Nemate dozvolu za ovu akciju.';
        } else if (error.status === 404) {
          errorMessage = 'Resurs nije pronađen.';
        } else if (error.status === 500) {
          errorMessage = 'Greška na serveru. Pokušajte ponovo.';
        } else {
          errorMessage = error.error?.message || error.message || `Server je vratio kod ${error.status}.`;
        }
      }

      console.error('HTTP Error:', {
        status: error.status,
        statusText: error.statusText,
        message: errorMessage,
        error: error.error,
        url: error.url
      });

      // Throw error with formatted message
      return throwError(() => ({
        ...error,
        message: errorMessage
      }));
    })
  );
};
