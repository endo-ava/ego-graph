import { cva } from 'class-variance-authority';

export const badgeVariants = cva(
  'inline-flex items-center gap-1.5 rounded-full border px-2.5 py-0.5 text-xs font-medium transition-colors focus:outline-none focus:ring-2 focus:ring-ring focus:ring-offset-2',
  {
    variants: {
      variant: {
        default: 'border-transparent bg-secondary text-secondary-foreground',
        primary: 'border-transparent bg-accent text-accent-foreground',
        destructive: 'border-transparent bg-destructive text-destructive-foreground',
        outline: 'text-foreground border-border',
        success: 'border-transparent bg-success/10 text-success border-success/20',
      },
    },
    defaultVariants: {
      variant: 'default',
    },
  },
);
