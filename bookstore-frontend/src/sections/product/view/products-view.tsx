import Box from '@mui/material/Box';
import Card from '@mui/material/Card';
import Grid from '@mui/material/Grid';
import Typography from '@mui/material/Typography';

import { DashboardContent } from 'src/layouts/dashboard';

import { ProductItem } from '../product-item';

import type { ProductItemProps } from '../product-item';

export function ProductsView() {
  const products: ProductItemProps[] = [];

  return (
    <DashboardContent>
      <Typography variant="h4" sx={{ mb: 5 }}>
        Products
      </Typography>
      {products.length === 0 ? (
        <Card sx={{ p: 6 }}>
          <Box sx={{ color: 'text.secondary', textAlign: 'center' }}>
            Product 데이터가 없습니다.
          </Box>
        </Card>
      ) : (
        <Grid container spacing={3}>
          {products.map((product) => (
            <Grid key={product.id} size={{ xs: 12, sm: 6, md: 3 }}>
              <ProductItem product={product} />
            </Grid>
          ))}
        </Grid>
      )}
    </DashboardContent>
  );
}
