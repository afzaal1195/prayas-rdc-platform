import axios from 'axios';

// Shape of what GlobalExceptionHandler actually returns (a Spring
// ProblemDetail, optionally with an "errors" map of field -> message for
// validation failures). Falls back to a generic message only when the
// response genuinely doesn't contain anything useful (network down, CORS
// blocked, etc.) -- never shows that as if it were the specific problem.
interface BackendProblem {
  detail?: string;
  errors?: Record<string, string>;
}

export function extractErrorMessage(err: unknown): string {
  if (axios.isAxiosError(err)) {
    if (!err.response) {
      return "Couldn't reach the server. Check your connection and try again.";
    }
    const data = err.response.data as BackendProblem | undefined;
    if (data?.errors && Object.keys(data.errors).length > 0) {
      return Object.values(data.errors).join(' ');
    }
    if (data?.detail) {
      return data.detail;
    }
    return `Request failed (${err.response.status}). Please try again.`;
  }
  return 'An unexpected error occurred. Please try again.';
}