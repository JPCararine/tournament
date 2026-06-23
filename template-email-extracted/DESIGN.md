---
name: Apex Narrative
colors:
  surface: '#0b1326'
  surface-dim: '#0b1326'
  surface-bright: '#31394d'
  surface-container-lowest: '#060e20'
  surface-container-low: '#131b2e'
  surface-container: '#171f33'
  surface-container-high: '#222a3d'
  surface-container-highest: '#2d3449'
  on-surface: '#dae2fd'
  on-surface-variant: '#cbc3d7'
  inverse-surface: '#dae2fd'
  inverse-on-surface: '#283044'
  outline: '#958ea0'
  outline-variant: '#494454'
  surface-tint: '#d0bcff'
  primary: '#d0bcff'
  on-primary: '#3c0091'
  primary-container: '#a078ff'
  on-primary-container: '#340080'
  inverse-primary: '#6d3bd7'
  secondary: '#4edea3'
  on-secondary: '#003824'
  secondary-container: '#00a572'
  on-secondary-container: '#00311f'
  tertiary: '#ffb2b7'
  on-tertiary: '#67001b'
  tertiary-container: '#ff516a'
  on-tertiary-container: '#5b0017'
  error: '#ffb4ab'
  on-error: '#690005'
  error-container: '#93000a'
  on-error-container: '#ffdad6'
  primary-fixed: '#e9ddff'
  primary-fixed-dim: '#d0bcff'
  on-primary-fixed: '#23005c'
  on-primary-fixed-variant: '#5516be'
  secondary-fixed: '#6ffbbe'
  secondary-fixed-dim: '#4edea3'
  on-secondary-fixed: '#002113'
  on-secondary-fixed-variant: '#005236'
  tertiary-fixed: '#ffdadb'
  tertiary-fixed-dim: '#ffb2b7'
  on-tertiary-fixed: '#40000d'
  on-tertiary-fixed-variant: '#92002a'
  background: '#0b1326'
  on-background: '#dae2fd'
  surface-variant: '#2d3449'
typography:
  display-lg:
    fontFamily: Montserrat
    fontSize: 48px
    fontWeight: '800'
    lineHeight: '1.1'
    letterSpacing: -0.02em
  display-lg-mobile:
    fontFamily: Montserrat
    fontSize: 32px
    fontWeight: '800'
    lineHeight: '1.2'
  headline-md:
    fontFamily: Montserrat
    fontSize: 24px
    fontWeight: '700'
    lineHeight: '1.3'
  body-lg:
    fontFamily: Hanken Grotesk
    fontSize: 18px
    fontWeight: '400'
    lineHeight: '1.6'
  body-md:
    fontFamily: Hanken Grotesk
    fontSize: 16px
    fontWeight: '400'
    lineHeight: '1.5'
  label-caps:
    fontFamily: JetBrains Mono
    fontSize: 12px
    fontWeight: '600'
    lineHeight: '1.0'
    letterSpacing: 0.1em
rounded:
  sm: 0.125rem
  DEFAULT: 0.25rem
  md: 0.375rem
  lg: 0.5rem
  xl: 0.75rem
  full: 9999px
spacing:
  base: 8px
  container-max: 1280px
  gutter: 24px
  margin-mobile: 16px
  margin-desktop: 40px
---

## Brand & Style

The design system is engineered for the high-stakes environment of competitive gaming and championship orchestration. It balances the raw energy of esports with the administrative precision required for tournament management. The aesthetic is "Cyber-Industrial"—combining dark, immersive backgrounds with high-frequency neon accents to create a sense of digital urgency and prestige.

The style leverages **High-Contrast Minimalism** fused with **Modern Glassmorphism**. This ensures that while the interface feels atmospheric and "gamer-centric," the data remains the primary focus. Interfaces should evoke a "Command Center" feel: authoritative, real-time, and technologically advanced.

## Colors

The palette is optimized for OLED displays and long-duration focus. 
- **Primary (Electric Purple):** Used for primary actions, branding elements, and active states. It represents the "energy" of the competition.
- **Secondary (Neon Green):** Reserved exclusively for "Confirmed," "Success," and "Live" status indicators. 
- **Tertiary (Cyber Rose):** Used for "Eliminated" states, critical alerts, and destructive actions.
- **Neutral (Deep Slate):** The foundation of the UI. Backgrounds use the darkest shades (#020617), while cards and surfaces use slightly lighter slate tones to create depth without losing the dark-mode immersion.
- **Text:** High-contrast White (#F8FAFC) for headings and Silver-Gray (#94A3B8) for secondary metadata.

## Typography

This design system utilizes a three-tier type system to differentiate between branding, reading, and technical data.
- **Headlines (Montserrat):** Bold, geometric, and aggressive. Use "ExtraBold" for tournament titles and "Bold" for section headers.
- **Body (Hanken Grotesk):** A sharp, contemporary sans-serif that maintains high legibility in dense data tables and rules sections.
- **Data (JetBrains Mono):** Used for technical metadata like match IDs, timestamps, and seed numbers. The monospaced nature reinforces the "system/technical" feel of the platform.

## Layout & Spacing

The layout follows a **Fluid Grid** model with strict 8px increments. 
- **Desktop:** A 12-column grid with generous 24px gutters. Use wide margins (40px) to give the dark UI breathing room.
- **Tablet:** 8-column grid. 
- **Mobile:** 4-column grid with 16px margins. 

Layouts should prioritize vertical stacking for tournament brackets and horizontal density for team tables. Use "Full-Bleed" sections for immersive headers, but contain management forms and tables within the `container-max` width to ensure scanability.

## Elevation & Depth

Depth is achieved through **Glassmorphism** and **Luminescent Borders** rather than traditional shadows.
- **Surface Layer:** The main background is a solid `#020617`.
- **Card Layer:** Semi-transparent slate (`rgba(30, 41, 59, 0.7)`) with a `backdrop-filter: blur(12px)`.
- **Border Hierarchy:** Elements do not use shadows. Instead, they use a 1px inner stroke. 
- **Active State:** Confirmed teams or active matches receive a `1px` solid primary or secondary border with a subtle outer glow (`box-shadow: 0 0 10px rgba(primary, 0.3)`).
- **Z-Index:** Modals and dropdowns should feel like floating HUD elements over the blurred background.

## Shapes

The shape language is "Tactical." While not entirely sharp, the roundedness is kept minimal to maintain a professional, high-performance feel. 
- **Standard Radius:** 4px for buttons and input fields.
- **Container Radius:** 8px for cards and table containers.
- **Status Pills:** Fully rounded (pill-shaped) to distinguish status indicators from functional buttons.
- **Interactive Elements:** Use chamfered corners (45-degree cuts) on decorative elements or large section headers to reinforce the gaming aesthetic.

## Components

### Buttons
- **Primary:** Solid Electric Purple with white bold text. Sharp 4px corners. 
- **Secondary:** Transparent with a 1px primary border. On hover, apply a soft purple background tint.
- **Action Icons:** Square buttons with centered icons, using the JetBrains Mono font for any associated technical shorthand.

### Data Tables
- **Header:** Darker than the card background, uppercase `label-caps` typography.
- **Rows:** Subtle 1px bottom border (`rgba(255,255,255,0.05)`). Hover state should brighten the entire row background.
- **Cells:** High-contrast white for team names; primary color for "Win" stats.

### Forms & Inputs
- **Inputs:** Dark slate background with a 1px border. The border glows Electric Purple on focus.
- **Validation:** Use Cyber Rose for error text and Neon Green for successful validation. Error messages appear directly below the field in `label-caps`.

### Cards & Status
- **Tournament Cards:** Glassmorphic background. If the tournament is "Live," apply a pulsing Neon Green 2px left-border.
- **Status Indicators:** Small, pill-shaped chips. Use Neon Green for "Confirmed," Yellow for "Pending," and Red for "Full/Closed."

### Rules & Terms Section
- Use a dedicated "Reader Mode" container with increased line-height (1.8) and Hanken Grotesk typography for long-form readability. Use primary color for sub-headers and bullet points to break up density.