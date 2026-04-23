/** Generic paginated response returned by backend collection endpoints. */
export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}

export type PaginatedApiResponse<T> = T[] | PageResponse<T>;

/**
 * Accepts both paginated and legacy list responses while local backend instances are refreshed.
 */
export function normalizePageResponse<T>(
  response: PaginatedApiResponse<T>,
  page: number,
  size: number,
): PageResponse<T> {
  if (Array.isArray(response)) {
    const start = page * size;
    const content = response.slice(start, start + size);

    return {
      content,
      page,
      size,
      totalElements: response.length,
      totalPages: Math.ceil(response.length / size),
      first: page === 0,
      last: start + size >= response.length,
    };
  }

  return {
    ...response,
    content: response.content ?? [],
    page: response.page ?? page,
    size: response.size ?? size,
    totalElements: response.totalElements ?? response.content?.length ?? 0,
    totalPages: response.totalPages ?? 1,
    first: response.first ?? page === 0,
    last: response.last ?? true,
  };
}
