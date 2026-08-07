export interface ApiResponse<T> {
  success: boolean;
  data: T;
  message: string | null;
  errors: string[];
  meta: Record<string, unknown>;
}
