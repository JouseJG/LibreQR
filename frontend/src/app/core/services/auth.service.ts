import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { API_BASE_URL } from '../config/api.config';
import { problemMessage } from '../models/api.models';

export type Credentials = {
  email: string;
  password: string;
  name?: string;
};

/**
 * Estado de sesión. La cookie `libreqr_sid` es HttpOnly: el frontend no la lee;
 * sabe si está autenticado tras el login o al sondear la API (401 = fuera).
 * El backend no expone `/me`, así que el nombre de usuario no se muestra.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);

  private readonly loggedInSignal = signal(false);
  private readonly checkedSignal = signal(false);
  private probe: Promise<void> | null = null;

  readonly loggedIn = this.loggedInSignal.asReadonly();
  readonly sessionChecked = this.checkedSignal.asReadonly();

  /** Comprueba la sesion una sola vez por carga de app (sondeo barato paginado). */
  ensureSession(): Promise<void> {
    this.probe ??= (async () => {
      try {
        await firstValueFrom(this.http.get(`${API_BASE_URL}/campaigns?page=0&size=1`));
        this.loggedInSignal.set(true);
      } catch {
        this.loggedInSignal.set(false);
      } finally {
        this.checkedSignal.set(true);
      }
    })();
    return this.probe;
  }

  async login(credentials: Credentials): Promise<void> {
    try {
      await firstValueFrom(
        this.http.post(`${API_BASE_URL}/auth/login`, {
          email: credentials.email,
          password: credentials.password,
        }),
      );
    } catch (error) {
      throw new Error(problemMessage(error, 'Credenciales incorrectas'));
    }
    this.loggedInSignal.set(true);
  }

  async register(credentials: Credentials): Promise<void> {
    try {
      await firstValueFrom(
        this.http.post(`${API_BASE_URL}/auth/register`, {
          email: credentials.email,
          name: credentials.name || null,
          password: credentials.password,
        }),
      );
    } catch (error) {
      throw new Error(problemMessage(error, 'No se pudo completar el registro'));
    }
  }

  /** El backend borra la sesion y la cookie; aqui solo se limpia el estado local. */
  async logout(): Promise<void> {
    try {
      await firstValueFrom(this.http.post(`${API_BASE_URL}/auth/logout`, {}));
    } finally {
      this.markLoggedOut();
    }
  }

  markLoggedOut(): void {
    this.loggedInSignal.set(false);
    this.checkedSignal.set(true);
  }
}
