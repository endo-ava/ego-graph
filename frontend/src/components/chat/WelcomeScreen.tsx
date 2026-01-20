export function WelcomeScreen() {
  const buildTime = import.meta.env.VITE_BUILD_TIME;

  return (
    <section className="relative flex h-full items-center justify-center overflow-hidden p-8">
      <div className="w-full max-w-lg">
        <PatternHexagon buildTime={buildTime} />
      </div>
    </section>
  );
}

// Pattern 1: Neon Hexagon (The Core)
export function PatternHexagon({ buildTime }: { buildTime?: string }) {
  return (
    <div className="flex flex-col items-center gap-8 animate-fade-in">
      <div className="relative">
        {/* Outer Glow */}
        <div className="absolute inset-0 bg-accent/20 blur-[60px] rounded-full opacity-50 animate-pulse" />

        {/* Main Hexagon */}
        <div className="relative h-32 w-32 flex items-center justify-center">
          <svg
            viewBox="0 0 100 100"
            className="h-full w-full drop-shadow-[0_0_15px_oklch(from_var(--color-accent)_l_c_h_/_0.5)]"
          >
            <path
              d="M50 5 L90 27.5 L90 72.5 L50 95 L10 72.5 L10 27.5 Z"
              fill="none"
              stroke="currentColor"
              strokeWidth="2"
              className="text-accent/80 drop-shadow-[0_0_10px_currentColor]"
            />
          </svg>

          {/* Inner Geometric Element */}
          <div className="absolute inset-0 flex items-center justify-center animate-[spin_10s_linear_infinite]">
            <svg viewBox="0 0 100 100" className="h-20 w-20 opacity-70">
              <path
                d="M50 20 L80 35 L80 65 L50 80 L20 65 L20 35 Z"
                fill="none"
                stroke="currentColor"
                strokeWidth="1"
                className="text-accent-foreground dark:text-accent"
              />
            </svg>
          </div>

          {/* Center Dot */}
          <div className="absolute h-2 w-2 bg-accent rounded-full shadow-[0_0_10px_currentColor] animate-ping" />
        </div>
      </div>

      <div className="flex flex-col items-center gap-3 text-center">
        <h2 className="text-3xl font-bold tracking-tight bg-clip-text text-transparent bg-gradient-to-b from-foreground to-foreground/70">
          EgoGraph
        </h2>
        <p className="text-base text-muted-foreground flex items-center gap-2">
          <span className="inline-block w-1.5 h-1.5 rounded-full bg-accent animate-pulse" />
          Ready to archive your digital life
        </p>
        {buildTime && (
          <p className="font-mono text-xs text-muted-foreground/50 mt-4">
            v{buildTime}
          </p>
        )}
      </div>
    </div>
  );
}
