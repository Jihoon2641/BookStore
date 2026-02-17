import { SvgColor } from 'src/components/svg-color';

// ----------------------------------------------------------------------

const icon = (name: string) => <SvgColor src={`/assets/icons/navbar/${name}.svg`} />;

export type NavItem = {
  title: string;
  path?: string;
  icon?: React.ReactNode;
  info?: React.ReactNode;
  children?: NavItem[];
};

export const navData = [
  {
    title: 'Dashboard',
    path: '/',
    icon: icon('ic-analytics'),
  },
  {
    title: 'System',
    path: '/system',
    icon: icon('ic-lock'),
    children: [
      {
        title: 'DB (HikariCP)',
        path: '/system/db',
      },
      {
        title: 'JVM',
        path: '/system/jvm',
      },
    ],
  },
  {
    title: 'Management',
    icon: icon('ic-user'),
    children: [
      {
        title: 'User',
        path: '/user',
      },
      {
        title: 'Product',
        path: '/products',
      },
    ],
  },
];
