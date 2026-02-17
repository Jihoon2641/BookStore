import type { Theme, SxProps, Breakpoint } from '@mui/material/styles';

import { useState, useEffect } from 'react';
import { varAlpha } from 'minimal-shared/utils';

import Box from '@mui/material/Box';
import Collapse from '@mui/material/Collapse';
import ListItem from '@mui/material/ListItem';
import { useTheme } from '@mui/material/styles';
import IconButton from '@mui/material/IconButton';
import ListItemButton from '@mui/material/ListItemButton';
import Drawer, { drawerClasses } from '@mui/material/Drawer';

import { usePathname } from 'src/routes/hooks';
import { RouterLink } from 'src/routes/components';

import { Logo } from 'src/components/logo';
import { Iconify } from 'src/components/iconify';
import { Scrollbar } from 'src/components/scrollbar';

import { NavUpgrade } from '../components/nav-upgrade';
import { WorkspacesPopover } from '../components/workspaces-popover';

import type { NavItem } from '../nav-config-dashboard';
import type { WorkspacesPopoverProps } from '../components/workspaces-popover';

// ----------------------------------------------------------------------

export type NavContentProps = {
  data: NavItem[];
  slots?: {
    topArea?: React.ReactNode;
    bottomArea?: React.ReactNode;
  };
  workspaces: WorkspacesPopoverProps['data'];
  sx?: SxProps<Theme>;
};

export function NavDesktop({
  sx,
  data,
  slots,
  workspaces,
  layoutQuery,
}: NavContentProps & { layoutQuery: Breakpoint }) {
  const theme = useTheme();

  return (
    <Box
      sx={{
        pt: 2.5,
        px: 2.5,
        top: 0,
        left: 0,
        height: 1,
        display: 'none',
        position: 'fixed',
        flexDirection: 'column',
        zIndex: 'var(--layout-nav-zIndex)',
        width: 'var(--layout-nav-vertical-width)',
        borderRight: `1px solid ${varAlpha(theme.vars.palette.grey['500Channel'], 0.12)}`,
        [theme.breakpoints.up(layoutQuery)]: {
          display: 'flex',
        },
        ...sx,
      }}
    >
      <NavContent data={data} slots={slots} workspaces={workspaces} />
    </Box>
  );
}

// ----------------------------------------------------------------------

export function NavMobile({
  sx,
  data,
  open,
  slots,
  onClose,
  workspaces,
}: NavContentProps & { open: boolean; onClose: () => void }) {
  const pathname = usePathname();

  useEffect(() => {
    if (open) {
      onClose();
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [pathname]);

  return (
    <Drawer
      open={open}
      onClose={onClose}
      sx={{
        [`& .${drawerClasses.paper}`]: {
          pt: 2.5,
          px: 2.5,
          overflow: 'unset',
          width: 'var(--layout-nav-mobile-width)',
          ...sx,
        },
      }}
    >
      <NavContent data={data} slots={slots} workspaces={workspaces} />
    </Drawer>
  );
}

// ----------------------------------------------------------------------

export function NavContent({ data, slots, workspaces, sx }: NavContentProps) {
  const pathname = usePathname();
  const [expanded, setExpanded] = useState<Record<string, boolean>>({});

  useEffect(() => {
    setExpanded((previous) => {
      const next = { ...previous };
      let changed = false;

      data.forEach((item) => {
        const hasChildren = !!item.children?.length;
        const itemKey = item.path ?? item.title;
        const isPathMatched = item.path
          ? pathname === item.path || pathname.startsWith(`${item.path}/`)
          : item.children?.some((child) => child.path === pathname);

        if (hasChildren && isPathMatched && !next[itemKey]) {
          next[itemKey] = true;
          changed = true;
        }
      });

      return changed ? next : previous;
    });
  }, [data, pathname]);

  return (
    <>
      <Logo />

      {slots?.topArea}

      <WorkspacesPopover data={workspaces} sx={{ my: 2 }} />

      <Scrollbar fillContent>
        <Box
          component="nav"
          sx={[
            {
              display: 'flex',
              flex: '1 1 auto',
              flexDirection: 'column',
            },
            ...(Array.isArray(sx) ? sx : [sx]),
          ]}
        >
          <Box
            component="ul"
            sx={{
              gap: 0.5,
              display: 'flex',
              flexDirection: 'column',
            }}
          >
            {data.map((item) => {
              const itemKey = item.path ?? item.title;
              const children = item.children ?? [];
              const linkChildren = children.filter((child): child is NavItem & { path: string } =>
                Boolean(child.path)
              );
              const hasChildren = children.length > 0;
              const isActived = item.path ? item.path === pathname : false;
              const isChildActived = hasChildren && children.some((child) => child.path === pathname);
              const isExpanded = hasChildren ? (expanded[itemKey] ?? false) : false;
              const handleToggleExpand = () => {
                setExpanded((previous) => ({
                  ...previous,
                  [itemKey]: !(previous[itemKey] ?? isChildActived),
                }));
              };
              const isLinkItem = !!item.path;

              return (
                <ListItem disableGutters disablePadding key={item.title} sx={{ display: 'block' }}>
                  <ListItemButton
                    disableGutters
                    {...(isLinkItem && { component: RouterLink, href: item.path })}
                    onClick={(event) => {
                      if (hasChildren && !isLinkItem) {
                        event.preventDefault();
                        handleToggleExpand();
                      }
                    }}
                    sx={[
                      (theme) => ({
                        pl: 2,
                        py: 1,
                        gap: 2,
                        pr: 1.5,
                        borderRadius: 0.75,
                        typography: 'body2',
                        fontWeight: 'fontWeightMedium',
                        color: theme.vars.palette.text.secondary,
                        minHeight: 44,
                        ...((isActived || isChildActived) && {
                          fontWeight: 'fontWeightSemiBold',
                          color: theme.vars.palette.primary.main,
                          bgcolor: varAlpha(theme.vars.palette.primary.mainChannel, 0.08),
                          '&:hover': {
                            bgcolor: varAlpha(theme.vars.palette.primary.mainChannel, 0.16),
                          },
                        }),
                      }),
                    ]}
                  >
                    <Box component="span" sx={{ width: 24, height: 24 }}>
                      {item.icon}
                    </Box>

                    <Box component="span" sx={{ flexGrow: 1 }}>
                      {item.title}
                    </Box>

                    {hasChildren && (
                      <IconButton
                        size="small"
                        color="inherit"
                        onClick={(event) => {
                          event.preventDefault();
                          event.stopPropagation();
                          handleToggleExpand();
                        }}
                      >
                        <Iconify
                          width={16}
                          icon={
                            isExpanded
                              ? 'eva:arrow-ios-upward-fill'
                              : 'eva:arrow-ios-downward-fill'
                          }
                        />
                      </IconButton>
                    )}

                    {item.info && item.info}
                  </ListItemButton>

                  {hasChildren && (
                    <Collapse in={isExpanded} timeout="auto" unmountOnExit>
                      <Box
                        component="ul"
                        sx={{
                          gap: 0.5,
                          mt: 0.5,
                          mb: 0.5,
                          pl: 1.25,
                          display: 'flex',
                          listStyle: 'none',
                          flexDirection: 'column',
                        }}
                      >
                        {linkChildren.map((child) => {
                          const isChildItemActived = child.path === pathname;

                          return (
                            <ListItem disableGutters disablePadding key={child.path}>
                              <ListItemButton
                                disableGutters
                                component={RouterLink}
                                href={child.path}
                                sx={[
                                  (theme) => ({
                                    py: 0.75,
                                    pl: 5,
                                    pr: 1.5,
                                    borderRadius: 0.75,
                                    typography: 'body2',
                                    minHeight: 38,
                                    color: theme.vars.palette.text.secondary,
                                    ...(isChildItemActived && {
                                      color: theme.vars.palette.primary.main,
                                      fontWeight: 'fontWeightSemiBold',
                                      bgcolor: varAlpha(theme.vars.palette.primary.mainChannel, 0.08),
                                    }),
                                  }),
                                ]}
                              >
                                <Box component="span" sx={{ flexGrow: 1 }}>
                                  {child.title}
                                </Box>
                              </ListItemButton>
                            </ListItem>
                          );
                        })}
                      </Box>
                    </Collapse>
                  )}
                </ListItem>
              );
            })}
          </Box>
        </Box>
      </Scrollbar>

      {slots?.bottomArea}

      <NavUpgrade />
    </>
  );
}
