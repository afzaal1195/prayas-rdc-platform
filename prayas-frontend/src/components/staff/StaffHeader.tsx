import type { ReactNode } from 'react';
import { StaffHeaderLogos } from './StaffHeaderLogos';
import '../../styles/form.css';
import '../../styles/admin.css';

interface Props {
  title: string;
  children?: ReactNode;
}

/** The top bar shared by every staff screen, including the sign-in page. */
export function StaffHeader({ title, children }: Props) {
  return (
    <header className="page-header">
      <StaffHeaderLogos />
      <figure className="header-quote">
        <blockquote>
          “Cultivation of mind should be the ultimate aim of human existence”
        </blockquote>
        <figcaption>Dr. B.R. Ambedkar</figcaption>
      </figure>
      <h1 className="staff-title">{title}</h1>
      {children}
    </header>
  );
}