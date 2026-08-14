type RoutePlaceholderProps = {
  title: string;
  description: string;
};

export function RoutePlaceholder({ title, description }: RoutePlaceholderProps) {
  return (
    <main className="shell">
      <p className="eyebrow">Contribution Attestation</p>
      <div className="card">
        <h1>{title}</h1>
        <p className="muted">{description}</p>
      </div>
    </main>
  );
}

