import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';

/**
 * Root application shell that delegates page rendering to the Angular router.
 */
@Component({
  selector: 'app-root',
  imports: [RouterOutlet],
  template: '<router-outlet />',
  styleUrl: './app.scss',
})
export class App {}
