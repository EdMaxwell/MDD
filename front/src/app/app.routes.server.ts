import { RenderMode, ServerRoute } from '@angular/ssr';

/**
 * SSR route rendering rules.
 *
 * Dynamic article detail pages are rendered on demand because their ids are not known at build time.
 */
export const serverRoutes: ServerRoute[] = [
  {
    path: 'articles/:id',
    renderMode: RenderMode.Server,
  },
  {
    path: '**',
    renderMode: RenderMode.Prerender,
  },
];
