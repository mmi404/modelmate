export function Footer() {
  return (
    <footer className="flex h-(--spacing-footer) shrink-0 items-center justify-center border-t border-border bg-background">
      <p className="text-sm text-muted-foreground">
        © {new Date().getFullYear()} ModelMate. All rights reserved.
      </p>
    </footer>
  );
}
