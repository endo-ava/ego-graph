/**
 * Buttonコンポーネント
 * shadcn/uiスタイルのボタン（手動実装）
 */

import * as React from 'react';
import { Slot } from '@radix-ui/react-slot';
import { cva, type VariantProps } from 'class-variance-authority';
import { cn } from '@/lib/utils';

const buttonVariants = cva(
  'inline-flex items-center justify-center gap-2 whitespace-nowrap rounded-md text-sm font-medium transition-all duration-200 ease-out focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:pointer-events-none disabled:opacity-50 [&_svg]:pointer-events-none [&_svg]:size-4 [&_svg]:shrink-0',
  {
    variants: {
      variant: {
        default:
          'bg-accent text-accent-foreground active:accent-glow active:-translate-y-px',
        destructive:
          'bg-destructive text-destructive-foreground active:opacity-90',
        outline:
          'border border-input bg-background active:bg-secondary',
        secondary:
          'bg-secondary text-secondary-foreground border border-border active:bg-muted',
        ghost: 'active:bg-muted active:text-foreground',
        link: 'text-primary underline-offset-4 active:underline',
      },
      size: {
        default: 'h-10 px-6 py-3',
        sm: 'h-8 px-4 py-2 text-xs',
        lg: 'h-12 px-8 py-4',
        icon: 'h-10 w-10',
      },
    },
    defaultVariants: {
      variant: 'default',
      size: 'default',
    },
  },
);

export interface ButtonProps
  extends React.ButtonHTMLAttributes<HTMLButtonElement>,
    VariantProps<typeof buttonVariants> {
  asChild?: boolean;
  ref?: React.Ref<HTMLButtonElement>;
}

const Button = ({ className, variant, size, asChild = false, ref, ...props }: ButtonProps) => {
  const Comp = asChild ? Slot : 'button';
  return (
    <Comp
      className={cn(buttonVariants({ variant, size, className }))}
      ref={ref}
      {...props}
    />
  );
};
Button.displayName = 'Button';

export { Button, buttonVariants };
