import { Injectable } from '@angular/core';
import { CardType } from '../models/payment.models';

@Injectable({
  providedIn: 'root'
})
export class CardValidationService {
  
 
  validateLuhn(pan: string): boolean {
    if (!pan) {
      return false;
    }
    
    const digits = pan.replace(/\D/g, '');
    
    if (!digits || !/^\d+$/.test(digits)) {
      return false;
    }
    
    let sum = 0;
    let doubleDigit = false;
    
    for (let i = digits.length - 1; i >= 0; i--) {
      let digit = parseInt(digits[i]);
      
      if (doubleDigit) {
        digit *= 2;
        if (digit > 9) {
          digit -= 9;
        }
      }
      
      sum += digit;
      doubleDigit = !doubleDigit;
    }
    
    return sum % 10 === 0;
  }
  
  validateExpirationDate(expirationDate: string): boolean {
    if (!expirationDate || !/^\d{2}\/\d{2}$/.test(expirationDate)) {
      return false;
    }
    
    try {
      const [monthStr, yearStr] = expirationDate.split('/');
      const month = parseInt(monthStr);
      const year = parseInt(yearStr);
      
      if (month < 1 || month > 12) {
        return false;
      }
      
      const now = new Date();
      const currentYear = now.getFullYear() % 100;
      const currentMonth = now.getMonth() + 1;
      
      if (year < currentYear) {
        return false; 
      }
      
      if (year === currentYear && month < currentMonth) {
        return false; 
      }
      
      return true;
    } catch (e) {
      return false;
    }
  }
  

  detectCardType(pan: string): CardType {
    if (!pan) {
      return null;
    }
    
    const digits = pan.replace(/\D/g, '');
    
    if (digits.startsWith('4')) {
      return 'VISA';
    } else if (digits.startsWith('5') || digits.startsWith('2')) {
      return 'MASTERCARD';
    }
    
    return null;
  }
  

  formatCardNumber(pan: string): string {
    const digits = pan.replace(/\D/g, '');
    return digits.match(/.{1,4}/g)?.join(' ') || digits;
  }
  

  maskCardNumber(pan: string): string {
    const digits = pan.replace(/\D/g, '');
    if (digits.length <= 4) {
      return digits;
    }
    return '**** **** **** ' + digits.slice(-4);
  }
}






