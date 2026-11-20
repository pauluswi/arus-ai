import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { LocalAuthService } from './local-auth.service';

export const mockIdentityInterceptor: HttpInterceptorFn = (request, next) => {
  const identity = inject(LocalAuthService).identity();
  if (!identity) return next(request);

  return next(
    request.clone({
      setHeaders: {
        'X-Mock-User': identity.username,
        'X-Mock-Roles': identity.roles.join(','),
      },
    }),
  );
};
